% http://www.cl.cam.ac.uk/~ab818/parseTraces.py
function ret = load_brajdic_bin(filename)

s = dir(filename);
l = 8 + 1 + 3 * 4;
n = s.bytes / l;

m = memmapfile(filename, 'format', {'int8', [l n], 'x'});

d = m.Data.x;
t = d(1:8,:);
type = d(9, :);
v = d(10:l, :);

% assuming little-endian hardware (swapbytes)
t = swapbytes(typecast(t(:), 'int64'));
v = reshape(swapbytes(typecast(v(:), 'single')), 3, n);

ret = steps.load_brajdic_ret(type, t, v);
