function ret = gwd(s)

% walk detection ground truth
ret = zeros(1, length(s.data.acc.t));
ret(s.wd(1):s.wd(2)) = 1;

% p02_1_tf: 325, 2.4
% p03_1_tf: 380, 2.06
