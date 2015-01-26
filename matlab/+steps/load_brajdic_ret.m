function ret = load_brajdic_ret(type, t, v)

t = t - t(1);
ret = struct;
for pair = {1 'acc'; 2 'gyr'; 3 'mag'}'
  i = type == pair{1};
  if isempty(i)
    continue;
  end
  field = pair{2};
  ret.(field) = struct('t', t(i), 'v', double(v(:, i)));
end
