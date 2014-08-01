% http://www.cl.cam.ac.uk/~ab818/parseTraces.py
function ret = load_brajdic_bin(filename)

data = struct;
file = fopen(filename, 'r', 'ieee-be.l64');
first = int64(-1);
while 1
  t = fread(file, 1, '*int64');
  if isempty(t)
    break;
  end
  typei = fread(file, 1, '*int8');
  v = fread(file, 3, 'float32');
  switch typei
    case 1
      type = 'acc';
    case 2
      type = 'gyr';
    case 3
      type = 'mag';
  end
  if first < 0
    first = t;
  end
  t = t - first;
  if isfield(data, type)
    data.(type).t{end+1} = t;
    data.(type).v{end+1} = v;
  else
    data.(type).t = {t};
    data.(type).v = {v};
  end
end

fields = fieldnames(data);
ret = struct;
for i = 1:size(fields, 1)
  field = fields{i};
  t = cell2mat(data.(field).t)';
  v = cell2mat(data.(field).v)';
  ret.(field) = table(t, v);
end
fclose(file);
