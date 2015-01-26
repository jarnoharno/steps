% http://www.cl.cam.ac.uk/~ab818/parseTraces.py
function ret = load_brajdic_txt(filename)

file = fopen(filename);
d = textscan(file, '%d64 %d %f %f %f');
fclose(file);

t = d{1}';
type = d{2};
v = [d{3:5}]';

ret = steps.load_brajdic_ret(type, t, v);
