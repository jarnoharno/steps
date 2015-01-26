% calculates average power of signal
% wnd - window in samples
% First values are calculated by repeating first samples.

function ret = magn(v, wnd)

G = 9.80665;

n = size(v, 1);
ret = zeros(n, 1);
sqn = sqrt(sum(v .* v, 2));
ret = sqn;
return;
sqn = [repmat(sqn(1), wnd - 1, 1); sqn];

for i = 1:n
  ret(i) = sum(sqn(i:(i + wnd - 1))) / wnd;
end
