function ret = mean(v, wnd)
ret = zeros(size(v));
data = [repmat(v(1, :), wnd - 1, 1); v];
for i = 1:size(v, 1)
  ret(i, :) = sum(data(i:(i + wnd - 1), :), 1) / wnd;
end
