function stdev = std_deviation(v, wnd)
% moving average
left = repmat(v(:, 1), 1, wnd);
right = repmat(v(:, end), 1, wnd);
data = [left v right];
ma = filter(ones(1, wnd), wnd, data')';
ma = [left ma(:, (wnd + 1):end)];
stdev = sqrt(filter(ones(1, wnd), wnd, ((data - ma).^2)'))';
stdev = stdev(:, (1:size(v, 2)) + floor(1.5 * wnd));
