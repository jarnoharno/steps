function vq = moving_average(v, wnd)
ret = zeros(size(v));
data = [repmat(v(:, 1), 1, wnd) v repmat(v(:, end), 1, wnd)]';
sm = filter(ones(1, wnd), wnd, data)';
vq = sm(:, ((wnd + 1):(wnd + size(v, 2))) + floor(wnd / 2));
