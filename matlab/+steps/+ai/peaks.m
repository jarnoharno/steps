function ret = peaks(v, wnd, min_dist)
i = 1;
ret = [];
while i + wnd - 1 <= length(v)
  [x t] = max(v(i:(i + wnd - 1)));
  ret = [ret; (i + t - 1)];
  i = i + t + min_dist;
end
if i <= length(v)
  [x t] = max(v(i:end));
  ret = [ret; (i + t - 1)];
end
