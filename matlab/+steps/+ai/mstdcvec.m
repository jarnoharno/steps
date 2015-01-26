function ret = mstdcvec(v, wnd)
wnd = steps.flatten(wnd);
ret = zeros(size(v, 1), length(wnd));
for i = 1:length(wnd)
  ret(:, i) = steps.ai.mstdc(v, wnd(i));
end
