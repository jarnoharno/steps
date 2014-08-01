function err = wderr(wd, wd_span, th)

% walk detection ground truth
gwd = zeros(1, size(wd, 1));
gwd(wd_span(1):wd_span(2)) = 1;

%step = .1;
%th_range = table((0:step:(max(x) + step))');

err = sum(table2array(rowfun(@(th) xor(wd' > th, gwd), table(th'))), 2);

% optimums:
% p02_1_tf:  356    2.5177
