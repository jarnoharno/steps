% optimize parameters for centered windowed standard deviation walk detection
function f = optmstdc(s)
f = @(x, y) steps.wderr(steps.ai.mstdc(s.data.acc.v, floor(x)*100), s.wd, y);
% p2_1_tf: 6.7216    2.2535
