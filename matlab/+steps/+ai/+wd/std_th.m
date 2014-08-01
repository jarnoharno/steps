function ret = std_th(s, wnd, th)
gwd = steps.gwd(s)';
m = steps.ai.mstdcvec(s.data.acc.v, wnd);

ret = zeros(length(wnd), length(th));
for i = 1:length(wnd)
  for j = 1:length(th)
    ret(i, j) = sum(xor(m(:, i) > th(j), gwd));
  end
end
ret = ret';
