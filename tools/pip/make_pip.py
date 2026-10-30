"""Builds Pip, the robot on a child's Home, as a glTF binary (app/src/main/assets/pip/pip.glb).

Pip is made of rounded shapes with physically based materials: brushed steel, clear-coated
paint, dark glass, rubber and lights. Every moving part is its own named node, and the app moves
them each frame (see app/src/main/java/app/stova/kids/PipScene.kt); the rest positions below are
the same numbers the app uses, so change both together.

Units are metres-ish, Y up, Z towards the camera, feet at y = 0.

    python tools/pip/make_pip.py
"""
import json, struct, math, os
import numpy as np

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'assets', 'pip', 'pip.glb')

# ---------------------------------------------------------------- geometry

def _sc(t, e):  # signed power of cos
    c = np.cos(t); return np.sign(c) * np.abs(c) ** e
def _ss(t, e):
    s = np.sin(t); return np.sign(s) * np.abs(s) ** e

def superellipsoid(rx, ry, rz, e1=1.0, e2=1.0, nu=40, nv=64, y_from=-math.pi / 2, y_to=math.pi / 2):
    """A rounded box (small e) or an ellipsoid (e = 1). Returns positions, normals, indices."""
    eta = np.linspace(y_from, y_to, nu + 1)
    omega = np.linspace(-math.pi, math.pi, nv + 1)
    E, W = np.meshgrid(eta, omega, indexing='ij')
    x = rx * _sc(E, e1) * _sc(W, e2)
    y = ry * _ss(E, e1)
    z = rz * _sc(E, e1) * _ss(W, e2)
    nx = _sc(E, 2 - e1) * _sc(W, 2 - e2) / rx
    ny = _ss(E, 2 - e1) / ry
    nz = _sc(E, 2 - e1) * _ss(W, 2 - e2) / rz
    P = np.stack([x, y, z], -1).reshape(-1, 3)
    N = np.stack([nx, ny, nz], -1).reshape(-1, 3)
    N /= np.maximum(np.linalg.norm(N, axis=1, keepdims=True), 1e-9)
    # Poles: straight up and down.
    if y_to >= math.pi / 2 - 1e-6: N[nu * (nv + 1):] = [0, 1, 0]
    if y_from <= -math.pi / 2 + 1e-6: N[:nv + 1] = [0, -1, 0]
    return P, N, _fix_winding(P, N, _grid(nu, nv))

def _grid(nu, nv, wrap=True):
    idx = []
    for i in range(nu):
        for j in range(nv):
            a = i * (nv + 1) + j; b = a + nv + 1
            idx += [a, b, a + 1, a + 1, b, b + 1]
    return np.array(idx, dtype=np.uint32)

def sphere(r, n=24):
    return superellipsoid(r, r, r, 1, 1, n, n * 2)

def hemisphere(r, top=True, n=16):
    return superellipsoid(r, r, r, 1, 1, n, n * 2, 0 if top else -math.pi / 2, math.pi / 2 if top else 0)

def cylinder(r, h, n=32, axis='y', rz=None):
    """A capped cylinder from 0 to h along the axis, or centred (both ends) when h < 0."""
    rz = rz or r
    y0, y1 = (0, h) if h > 0 else (h / 2, -h / 2)
    ang = np.linspace(0, 2 * math.pi, n + 1)
    ring = np.stack([r * np.cos(ang), np.zeros_like(ang), rz * np.sin(ang)], -1)
    nrm = np.stack([np.cos(ang) / r, np.zeros_like(ang), np.sin(ang) / rz], -1)
    nrm /= np.linalg.norm(nrm, axis=1, keepdims=True)
    P = []; N = []; I = []
    for y in (y0, y1):
        P.append(ring + [0, y, 0]); N.append(nrm)
    P = np.concatenate(P); N = np.concatenate(N)
    for j in range(n):
        a, b = j, j + n + 1
        I += [a, b, a + 1, a + 1, b, b + 1]
    # Caps.
    for y, up in ((y0, -1), (y1, 1)):
        base = len(P)
        P = np.concatenate([P, [[0, y, 0]], ring + [0, y, 0]])
        N = np.concatenate([N, np.tile([0, up, 0], (n + 2, 1))])
        for j in range(n):
            I += [base, base + 1 + j, base + 2 + j] if up < 0 else [base, base + 2 + j, base + 1 + j]
    P, N = np.array(P, float), np.array(N, float)
    if axis == 'x': P, N = P[:, [1, 0, 2]] * [1, 1, 1], N[:, [1, 0, 2]]
    if axis == 'z': P, N = P[:, [0, 2, 1]], N[:, [0, 2, 1]]
    return P, N, _fix_winding(P, N, np.array(I, np.uint32))

def torus(R, r, nu=48, nv=16, axis='z', squash=1.0):
    """A ring around the axis (default: facing the camera)."""
    u = np.linspace(0, 2 * math.pi, nu + 1); v = np.linspace(0, 2 * math.pi, nv + 1)
    U, V = np.meshgrid(u, v, indexing='ij')
    x = (R + r * np.cos(V)) * np.cos(U); y = (R + r * np.cos(V)) * np.sin(U) * squash; z = r * np.sin(V)
    nx = np.cos(V) * np.cos(U); ny = np.cos(V) * np.sin(U); nz = np.sin(V)
    P = np.stack([x, y, z], -1).reshape(-1, 3); N = np.stack([nx, ny, nz], -1).reshape(-1, 3)
    if axis == 'y': P, N = P[:, [0, 2, 1]], N[:, [0, 2, 1]]
    return P, N, _fix_winding(P, N, _grid(nu, nv))

def sweep(spine, radii, n=28, zscale=1.0, cap=True):
    """A tube along a spine of points, its radius changing along the way; closed at both ends."""
    spine = np.array(spine, float); radii = np.array(radii, float)
    T = np.gradient(spine, axis=0); T /= np.linalg.norm(T, axis=1, keepdims=True)
    ref = np.array([0, 0, 1.0])
    Nn = np.cross(T, ref); Nn /= np.maximum(np.linalg.norm(Nn, axis=1, keepdims=True), 1e-9)
    B = np.cross(Nn, T)
    ang = np.linspace(0, 2 * math.pi, n + 1)
    P = []; N = []
    for i in range(len(spine)):
        for a in ang:
            d = Nn[i] * math.cos(a) + B[i] * math.sin(a) * zscale
            P.append(spine[i] + d * radii[i]); nd = Nn[i] * math.cos(a) + B[i] * math.sin(a) / zscale
            N.append(nd / np.linalg.norm(nd))
    P = np.array(P); N = np.array(N); I = list(_grid(len(spine) - 1, n))
    if cap:
        for end, k in ((0, -1), (len(spine) - 1, 1)):
            base = len(P); P = np.concatenate([P, [spine[end]]]); N = np.concatenate([N, [T[end] * k]])
            row = end * (n + 1)
            for j in range(n):
                I += [base, row + j, row + j + 1] if k > 0 else [base, row + j + 1, row + j]
    return P, N, _fix_winding(P, N, np.array(I, np.uint32))

def box(w, h, d, e=0.25, n=20):
    return superellipsoid(w / 2, h / 2, d / 2, e, e, n, n * 2)

def _fix_winding(P, N, I):
    """Turns each triangle so it faces along its vertex normals (glTF wants counter-clockwise outside)."""
    t = I.reshape(-1, 3)
    a, b, c = P[t[:, 0]], P[t[:, 1]], P[t[:, 2]]
    face = np.cross(b - a, c - a)
    flip = np.einsum('ij,ij->i', face, N[t[:, 0]] + N[t[:, 1]] + N[t[:, 2]]) < 0
    t[flip] = t[flip][:, [0, 2, 1]]
    return t.reshape(-1).astype(np.uint32)

def move(mesh, dx=0, dy=0, dz=0, sx=1, sy=1, sz=1):
    P, N, I = mesh
    P = P * [sx, sy, sz] + [dx, dy, dz]
    N = N / np.array([sx, sy, sz]); N /= np.maximum(np.linalg.norm(N, axis=1, keepdims=True), 1e-9)
    return P, N, I

def merge(*meshes):
    P = []; N = []; I = []; off = 0
    for p, n, i in meshes:
        P.append(p); N.append(n); I.append(i + off); off += len(p)
    return np.concatenate(P), np.concatenate(N), np.concatenate(I).astype(np.uint32)

# ---------------------------------------------------------------- glTF

class Glb:
    def __init__(self):
        self.bin = bytearray(); self.views = []; self.accessors = []; self.meshes = []
        self.nodes = []; self.materials = []; self.mat_index = {}

    def material(self, name, color, metal=0.0, rough=0.5, emissive=None, strength=1.0, clearcoat=None):
        m = {'name': name, 'pbrMetallicRoughness': {'baseColorFactor': list(color) + [1.0], 'metallicFactor': metal, 'roughnessFactor': rough}}
        ext = {}
        if emissive is not None:
            m['emissiveFactor'] = list(emissive)
            if strength != 1.0: ext['KHR_materials_emissive_strength'] = {'emissiveStrength': strength}
        if clearcoat is not None:
            ext['KHR_materials_clearcoat'] = {'clearcoatFactor': 1.0, 'clearcoatRoughnessFactor': clearcoat}
        if ext: m['extensions'] = ext
        self.mat_index[name] = len(self.materials); self.materials.append(m)

    def _view(self, data, target):
        while len(self.bin) % 4: self.bin.append(0)
        off = len(self.bin); self.bin += data
        self.views.append({'buffer': 0, 'byteOffset': off, 'byteLength': len(data), 'target': target})
        return len(self.views) - 1

    def mesh(self, name, parts):
        """parts: [(mesh, material name)]"""
        prims = []
        for (P, N, I), mat in parts:
            P = P.astype(np.float32); N = N.astype(np.float32); I = I.astype(np.uint32)
            pa = len(self.accessors)
            self.accessors.append({'bufferView': self._view(P.tobytes(), 34962), 'componentType': 5126, 'count': len(P), 'type': 'VEC3',
                                   'min': P.min(0).tolist(), 'max': P.max(0).tolist()})
            self.accessors.append({'bufferView': self._view(N.tobytes(), 34962), 'componentType': 5126, 'count': len(N), 'type': 'VEC3'})
            self.accessors.append({'bufferView': self._view(I.tobytes(), 34963), 'componentType': 5125, 'count': len(I), 'type': 'SCALAR'})
            prims.append({'attributes': {'POSITION': pa, 'NORMAL': pa + 1}, 'indices': pa + 2, 'material': self.mat_index[mat]})
        self.meshes.append({'name': name, 'primitives': prims})
        return len(self.meshes) - 1

    def node(self, name, parts=None, t=(0, 0, 0), children=()):
        n = {'name': name}
        if parts: n['mesh'] = self.mesh(name, parts)
        if any(t): n['translation'] = list(map(float, t))
        if children: n['children'] = list(children)
        self.nodes.append(n)
        return len(self.nodes) - 1

    def write(self, path, root):
        doc = {'asset': {'version': '2.0', 'generator': 'Stova tools/pip/make_pip.py'},
               'extensionsUsed': ['KHR_materials_emissive_strength', 'KHR_materials_clearcoat'],
               'scene': 0, 'scenes': [{'nodes': [root]}], 'nodes': self.nodes, 'meshes': self.meshes,
               'materials': self.materials, 'accessors': self.accessors, 'bufferViews': self.views,
               'buffers': [{'byteLength': len(self.bin)}]}
        js = json.dumps(doc, separators=(',', ':')).encode()
        while len(js) % 4: js += b' '
        while len(self.bin) % 4: self.bin.append(0)
        out = struct.pack('<III', 0x46546C67, 2, 12 + 8 + len(js) + 8 + len(self.bin))
        out += struct.pack('<II', len(js), 0x4E4F534A) + js + struct.pack('<II', len(self.bin), 0x004E4942) + bytes(self.bin)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'wb').write(out)
        return len(out)

# ---------------------------------------------------------------- Pip

def build():
    g = Glb()
    # Materials: names are what the app looks up (lights change their glow at run time).
    g.material('steel', (0.90, 0.91, 0.94), metal=1.0, rough=0.34)
    g.material('steel_dark', (0.46, 0.49, 0.55), metal=1.0, rough=0.42)
    g.material('paint', (0.16, 0.60, 0.55), metal=0.0, rough=0.42, clearcoat=0.08)
    g.material('glass', (0.015, 0.02, 0.035), metal=0.0, rough=0.06, clearcoat=0.02)
    g.material('eyeball', (0.01, 0.012, 0.018), metal=0.0, rough=0.12, clearcoat=0.02)
    g.material('iris', (0.10, 0.45, 0.60), emissive=(0.30, 0.85, 1.0), strength=5.0)
    g.material('pupil', (0.005, 0.006, 0.01), rough=0.35)
    g.material('glint', (1, 1, 1), emissive=(1, 1, 1), strength=8.0)
    g.material('rubber', (0.05, 0.055, 0.07), rough=0.85)
    g.material('mouth', (0.05, 0.2, 0.3), emissive=(0.30, 0.85, 1.0), strength=5.0)
    g.material('antenna_light', (0.6, 0.2, 0.1), emissive=(1.0, 0.42, 0.22), strength=7.0)
    g.material('heart', (0.6, 0.1, 0.15), emissive=(1.0, 0.28, 0.38), strength=6.0)
    g.material('cheek', (0.5, 0.25, 0.3), emissive=(1.0, 0.45, 0.55), strength=0.8)
    # What he wears.
    g.material('santa', (0.62, 0.06, 0.07), rough=0.9)
    g.material('fur', (0.93, 0.92, 0.90), rough=1.0)
    g.material('witch', (0.20, 0.10, 0.34), rough=0.8)
    g.material('witch_band', (0.95, 0.42, 0.05), rough=0.7)
    g.material('beanie', (0.12, 0.33, 0.78), rough=0.95)
    g.material('nightcap', (0.16, 0.20, 0.50), rough=0.9)
    g.material('star', (0.9, 0.75, 0.2), emissive=(1.0, 0.82, 0.35), strength=2.0)
    g.material('petal', (1.0, 0.80, 0.15), rough=0.6)
    g.material('petal_pink', (1.0, 0.50, 0.70), rough=0.6)
    g.material('leaf', (0.25, 0.62, 0.20), rough=0.7)
    g.material('lens', (0.02, 0.03, 0.05), rough=0.04, clearcoat=0.01)
    g.material('scarf', (0.90, 0.38, 0.10), rough=0.95)
    g.material('flag_red', (0.73, 0.03, 0.15), rough=0.7)
    g.material('flag_white', (0.95, 0.95, 0.95), rough=0.7)
    g.material('flag_blue', (0.0, 0.10, 0.36), rough=0.7)
    g.material('bunny_pink', (1.0, 0.66, 0.76), rough=0.9)

    # Head: everything is relative to the head node, which sits on the neck.
    # Eyes are lit shapes on the glass, like a friendly robot's screen: they squash to blink, and two
    # pieces of the same dark glass close over them from above and below for sleepy or happy eyes.
    eyes = []
    for side, name in ((-1, 'L'), (1, 'R')):
        shape = g.node('eyeShape' + name, [(superellipsoid(0.064, 0.080, 0.010, 0.42, 0.42, 20, 40), 'iris'),
                                            (move(sphere(0.016, 10), -0.022, 0.034, 0.010, sz=0.3), 'glint'),
                                            (move(sphere(0.008, 8), 0.024, -0.030, 0.010, sz=0.3), 'glint')])
        # Unit lids: from their edge 1 unit out; the app scales them to how far they close.
        lid_top = g.node('lidTop' + name, [(move(box(0.17, 1.0, 0.006, 0.12, 8), dy=-0.5, dz=0.014), 'glass')])
        lid_bot = g.node('lidBot' + name, [(move(box(0.17, 1.0, 0.006, 0.12, 8), dy=0.5, dz=0.014), 'glass')])
        eye = g.node('eye' + name, t=(side * 0.125, 0.255, 0.296), children=[shape, lid_top, lid_bot])
        eyes.append(eye)
    mouth = [g.node('mouth%d' % i, [(sphere(0.013, 10), 'mouth')], t=((i - 3) * 0.034, 0.145, 0.296)) for i in range(7)]
    ears = [g.node('ear' + n, [(move(cylinder(0.085, -0.07, 40, 'x'), dx=s * 0.375, dy=0.26), 'steel'),
                               (move(cylinder(0.066, -0.012, 32, 'x'), dx=s * 0.405, dy=0.26), 'steel_dark'),
                               (move(cylinder(0.052, -0.03, 32, 'x'), dx=s * 0.418, dy=0.26), 'paint')])
            for s, n in ((-1, 'L'), (1, 'R'))]
    flower = g.node('flower', [(move(sphere(0.028, 12), 0, 0.17, 0.0), 'petal')] +
                    [(move(sphere(0.03, 12), 0.042 * math.cos(a), 0.17 + 0.042 * math.sin(a), -0.005, sz=0.45), 'petal_pink' if k % 2 else 'petal')
                     for k, a in enumerate(np.linspace(0, 2 * math.pi, 6)[:5] + math.pi / 2)] +
                    [(move(sphere(0.03, 10), -0.03, 0.08, 0, sx=1.3, sy=0.45, sz=0.5), 'leaf')])
    flag = g.node('flag', [(move(box(0.13, 0.095, 0.006, 0.1, 6), 0.075, 0.13, 0), 'flag_red'),
                           (move(box(0.024, 0.095, 0.008, 0.1, 6), 0.05, 0.13, 0), 'flag_white'),
                           (move(box(0.13, 0.024, 0.008, 0.1, 6), 0.075, 0.13, 0), 'flag_white'),
                           (move(box(0.012, 0.095, 0.01, 0.1, 6), 0.05, 0.13, 0), 'flag_blue'),
                           (move(box(0.13, 0.012, 0.01, 0.1, 6), 0.075, 0.13, 0), 'flag_blue'),
                           (move(cylinder(0.006, 0.12), 0, 0.12, 0), 'steel')])
    bulb = g.node('bulb', [(move(sphere(0.034, 20), 0, 0.17, 0), 'antenna_light')])
    antenna = g.node('antenna', [(move(sphere(0.042, 20), sy=0.45), 'steel'), (cylinder(0.011, 0.15, 16), 'steel_dark')],
                     t=(0, 0.495, 0), children=[bulb, flower, flag])
    # The hats: a floppy cap that falls to one side, a witch's hat, a winter beanie, bunny ears.
    def floppy(cloth, bobble):
        spine = [(0, 0.40, 0), (0.01, 0.53, 0), (0.07, 0.66, 0), (0.19, 0.72, 0), (0.31, 0.66, 0), (0.37, 0.55, 0)]
        return [(sweep(spine, [0.355, 0.30, 0.20, 0.11, 0.055, 0.02], 40, zscale=0.8), cloth),
                (move(torus(0.355, 0.055, axis='y'), dy=0.40, sz=0.8), 'fur' if cloth == 'santa' else 'fur'),
                (move(sphere(0.06, 16), 0.375, 0.52, 0), bobble)]
    santa = g.node('santa', floppy('santa', 'fur'))
    nightcap = g.node('nightcap', floppy('nightcap', 'star'))
    witch = g.node('witch', [(move(cylinder(0.52, -0.016, 64), dy=0.43, sz=0.85), 'witch'),
                             (sweep([(0, 0.43, 0), (0, 0.6, 0), (0.02, 0.78, 0), (0.09, 0.92, 0), (0.16, 0.98, 0)],
                                    [0.27, 0.20, 0.12, 0.05, 0.012], 36, zscale=0.85), 'witch'),
                             (move(cylinder(0.262, -0.055, 48), dy=0.47, sz=0.85), 'witch_band')])
    beanie = g.node('beanie', [(move(hemisphere(0.38, True, 20), dy=0.38, sy=0.62, sz=0.80), 'beanie'),
                               (move(torus(0.37, 0.06, axis='y'), dy=0.40, sz=0.8), 'beanie'),
                               (move(sphere(0.075, 16), dy=0.64), 'fur')])
    bunny = g.node('bunny', [p for s in (-1, 1) for p in (
        (sweep([(s * 0.12, 0.46, 0), (s * 0.13, 0.62, 0), (s * 0.15, 0.78, 0), (s * 0.17, 0.88, 0)], [0.05, 0.065, 0.05, 0.02], 24, zscale=0.35), 'fur'),
        (sweep([(s * 0.12, 0.50, 0.014), (s * 0.13, 0.63, 0.016), (s * 0.15, 0.77, 0.016), (s * 0.165, 0.85, 0.012)], [0.025, 0.04, 0.03, 0.012], 20, zscale=0.3), 'bunny_pink'))])
    glasses = g.node('sunglasses', [(move(box(0.19, 0.13, 0.03, 0.35, 12), s * 0.135, 0.27, 0.36), 'lens') for s in (-1, 1)] +
                     [(move(cylinder(0.012, -0.08, 12, 'x'), 0, 0.29, 0.36), 'steel_dark')])
    head = g.node('head', [(move(superellipsoid(0.37, 0.25, 0.28, 0.55, 0.62, 48, 72), dy=0.25), 'steel'),
                           (move(superellipsoid(0.29, 0.168, 0.04, 0.42, 0.42, 32, 64), dy=0.235, dz=0.252), 'glass'),
                           (move(sphere(0.034, 14), -0.215, 0.17, 0.283, sx=1.3, sz=0.25), 'cheek'),
                           (move(sphere(0.034, 14), 0.215, 0.17, 0.283, sx=1.3, sz=0.25), 'cheek'),
                           (move(torus(0.31, 0.008, nu=64, axis='y'), dy=0.47, sz=0.76), 'steel_dark')],
                  t=(0, 0.42, 0), children=eyes + mouth + ears + [antenna, santa, nightcap, witch, beanie, bunny, glasses])
    # Body: relative to the pelvis, which the app bobs, leans and squashes.
    scarf = g.node('scarf', [(move(torus(0.11, 0.05, axis='y'), dy=0.375), 'scarf'),
                             (move(box(0.07, 0.20, 0.035, 0.35, 10), 0.06, 0.27, 0.20), 'scarf')])
    rosette = g.node('rosette', [(move(cylinder(0.05, -0.008, 32, 'z'), 0.12, 0.25, 0.19), 'flag_red'),
                                 (move(cylinder(0.034, -0.01, 32, 'z'), 0.12, 0.25, 0.192), 'flag_white'),
                                 (move(cylinder(0.018, -0.012, 24, 'z'), 0.12, 0.25, 0.194), 'flag_blue')])
    pelvis = g.node('pelvis', [(move(superellipsoid(0.23, 0.17, 0.18, 0.38, 0.45, 40, 64), dy=0.17), 'paint'),
                               (move(box(0.27, 0.20, 0.035, 0.3, 16), 0, 0.17, 0.168), 'steel'),
                               (move(torus(0.040, 0.010), 0, 0.15, 0.19), 'steel_dark'),
                               (move(sphere(0.032, 18), 0, 0.15, 0.188, sz=0.6), 'heart'),
                               ] + [(move(box(0.12, 0.010, 0.012, 0.3, 6), 0, 0.225 + k * 0.022, 0.19), 'steel_dark') for k in range(3)] +
                              [(move(cylinder(0.075, 0.075, 32), dy=0.335), 'rubber'),
                               (move(torus(0.078, 0.012, axis='y'), dy=0.355), 'rubber'),
                               (move(torus(0.074, 0.012, axis='y'), dy=0.385), 'rubber')],
                    t=(0, 0.30, 0), children=[head, scarf, rosette])
    # Limbs: unit pieces the app stretches between joints every frame.
    limbs = []
    for side in ('L', 'R'):
        for part, r in (('upperArm', 0.028), ('foreArm', 0.026), ('thigh', 0.036), ('shin', 0.032)):
            limbs.append(g.node(part + side, [(cylinder(r, -1.0, 20), 'steel')]))
        for part, r, mat in (('shoulder', 0.052, 'steel'), ('elbow', 0.034, 'steel_dark'), ('hand', 0.040, 'steel_dark'),
                             ('hip', 0.046, 'steel_dark'), ('knee', 0.040, 'steel_dark'), ('ankle', 0.030, 'steel_dark')):
            limbs.append(g.node(part + side, [(sphere(r, 16), mat)]))
        for k in range(3):
            # Fingers keep their length: the app only turns them.
            limbs.append(g.node('finger%d%s' % (k, side), [(cylinder(0.011, 0.05, 10), 'steel_dark'), (move(sphere(0.012, 8), dy=0.05), 'steel_dark')]))
        limbs.append(g.node('foot' + side, [(move(box(0.14, 0.055, 0.20, 0.35, 16), 0, 0.045, 0.02), 'paint'),
                                            (move(box(0.145, 0.03, 0.205, 0.3, 16), 0, 0.016, 0.02), 'rubber')]))
    root = g.node('pip', children=[pelvis] + limbs)
    return g, root

if __name__ == '__main__':
    g, root = build()
    size = g.write(OUT, root)
    print('wrote', os.path.normpath(OUT), size, 'bytes,', len(g.nodes), 'nodes,', len(g.meshes), 'meshes')
