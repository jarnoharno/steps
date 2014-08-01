function ret = load(filename)

data = struct;
file = fopen(filename);

while 1
  type = fscanf(file, '%s', 1);
  if isempty(type)
    break;
  end
  t = fscanf(file, '%ld', 1);
  v = fscanf(file, '%f'); % different types can have different number of values
  if isfield(data, type)
    data.(type).t{end+1} = t;
    data.(type).v{end+1} = v;
  else
    data.(type).t = {t};
    data.(type).v = {v};
  end
end
types = fieldnames(data);
ret = struct;
for i = 1:size(types, 1)
  type = types{i};
  ret.(type) = table(cell2mat(data.(type).t)', cell2mat(data.(type).v)', ...
    'VariableNames', {'t', 'v'});
end
fclose(file);
