function plotspacing(x, y, range)
plot(x(range), y(range,:));
for i = range
  hx = graph2d.constantline(x(i), 'LineStyle', ':', 'Color', [.5 .5 .5]);
  changedependvar(hx, 'x');
end
xlim([x(range(1)) x(range(end))])
