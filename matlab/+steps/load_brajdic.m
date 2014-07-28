% http://www.cl.cam.ac.uk/~ab818/parseTraces.py
function ret = load_brajdic(filename)
ret = struct;
file = fopen(filename, 'r', 'ieee-be.l64');
first = int64(-1);
while 1
  timestamp = fread(file, 1, '*int64');
  if isempty(timestamp)
    break;
  end
  typei = fread(file, 1, '*int8');
  values = fread(file, 3, 'float32');
  switch typei
    case 1
      type = 'acc';
    case 2
      type = 'gyr';
    case 3
      type = 'mag';
  end
  if first < 0
    first = timestamp;
  end
  timestamp = timestamp - first;
  if isfield(ret, type)
    ret.(type).timestamp = [ret.(type).timestamp timestamp];
    ret.(type).values = [ret.(type).values; values'];
  else
    ret.(type).timestamp = [timestamp];
    ret.(type).values = values';
  end
end
fclose(file);
