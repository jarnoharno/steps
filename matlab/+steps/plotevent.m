function plotevent(tab)
v = axis;
for i = 1:size(tab, 1)
  hx = graph2d.constantline(tab.t(i), 'LineStyle', ':', 'Color', [.5 .5 .5]);
  changedependvar(hx, 'x');
end
axis(v);
