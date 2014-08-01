% http://www.cl.cam.ac.uk/~ab818/parseTraces.py
function ret = load_brajdic_txt(filename)

data = readtable(filename, 'Delimiter', ' ', ...
  'ReadVariableName', false, 'Format', '%d64 %d %f %f %f');
data.Properties.VariableNames = {'timestamp', 'type', 'x', 'y', 'z'};

v = data{:, {'x', 'y', 'z'}};
t = data{:, 'timestamp'};
t = t - t(1);

acc = data.type == 1;
gyr = data.type == 2;
mag = data.type == 3;

ret = struct(...
  'acc', table(t(acc), v(acc, :), 'VariableNames', {'t', 'v'}), ...
  'gyr', table(t(gyr), v(gyr, :), 'VariableNames', {'t', 'v'}), ...
  'mag', table(t(mag), v(mag, :), 'VariableNames', {'t', 'v'}));
