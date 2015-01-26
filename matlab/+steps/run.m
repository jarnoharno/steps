function ret = run(t)
cmd = ['./steps runpull -t ' int2str(t)];
[status cmdout] = unix(cmd, '-echo');
match = regexp(cmdout, 'file=(\w*)', 'tokens');
file = ['data/' match{1}{1}];
ret = steps.load(file);
% plot - brittle code ahead
d = ret;
t = d.mad.t;
v = d.mad.v - mean(d.mad.v);
if isfield(d, 'map')
  plot(t, v, 'b', d.map.t, d.map.v - mean(d.mad.v) + 0.05, 'rv', 'markerfacecolor', 'r');
else
  plot(t, v, 'b');
end
steps.plotevent(d.stdth);
