function ret = read(filename)
ret = struct;
file = fopen(filename);
while 1
  type = fscanf(file, '%s', 1);
  if isempty(type)
    break;
  end
  timestamp = fscanf(file, '%ld', 1);
  values = fscanf(file, '%f');
  if isfield(ret, type)
    ret.(type).timestamp = [ret.(type).timestamp timestamp];
    ret.(type).values = [ret.(type).values; values'];
  else
    ret.(type).timestamp = [timestamp];
    ret.(type).values = values';
  end
end
fclose(file);
