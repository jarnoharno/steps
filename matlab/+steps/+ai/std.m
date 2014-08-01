function ret = std(v, wnd)
m = steps.ai.mean(v, wnd);
data = [repmat(v(1, :), wnd - 1, 1); v];
ret = zeros(size(v));
for i = 1:size(v, 1)
  vi = data(i:(i + wnd - 1), :);
  mi = repmat(m(i, :), wnd, 1);
  ret(i, :) = sqrt(sum((vi - mi).^2, 1) / wnd);
end
