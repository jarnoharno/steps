function [xq, vq] = resample(x, v, period, method)
if nargin < 4
  method = 'linear';
end
n = floor((x(end) - x(1)) / period);
xq = (0:n) * period + x(1);
vq = interp1(double(x), double(v'), double(xq), method)';
