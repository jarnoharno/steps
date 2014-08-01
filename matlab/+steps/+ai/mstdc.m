% centered windowed standard deviation of magnitude
function ret = mstdc(v, wnd)
delay = floor(wnd / 2);
magn = steps.ai.magn(v);
magn = [magn; repmat(magn(end), delay, 1)];
x = steps.ai.std(magn, wnd);
ret = x((delay + 1):end);
