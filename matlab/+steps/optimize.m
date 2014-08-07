function ret = optimize(varargin)

opts = struct('resample_rate', 1e7, 'dataset', [], 'std_win', 0.2:0.1:1.6,...
  'std_th', 0.2:0.1:4.0);

if mod(length(varargin), 2)
  error('odd number of arguments');
end
fields = fieldnames(opts);
for pair = reshape(varargin, 2, [])
  field = pair{1};
  if ~any(strcmp(field, fields))
    error('invalid parameter name %s', field);
  end
  opts.(field) = pair{2};
end

if isempty(opts.dataset)
  opts.dataset = steps.load_brajdic_all;
end

% param table
c = combvec(opts.std_th, opts.std_win)';
std_th = c(:, 1);
std_win = c(:, 2);
ptable = table(std_win, std_th);

% convert to samples
opts.std_win = round(opts.std_win * 1e9 / opts.resample_rate);

% prune dataset
traces = fieldnames(opts.dataset);
for tracei = 1:length(traces)
  trace = traces{tracei};
  if ~isfield(opts.dataset.(trace), 'wd')
    opts.dataset = rmfield(opts.dataset, trace);
  end
end
traces = fieldnames(opts.dataset);

nparams = length(opts.std_th) * length(opts.std_win);

err = zeros(nparams, length(traces));

for tracei = 1:length(traces);
  trace = opts.dataset.(traces{tracei});
  % resample
  [t v] = steps.ai.resample(trace.data.acc.t, trace.data.acc.v, opts.resample_rate);

  % find closest ground truth values to resampled data
  ti = trace.data.acc.t(trace.wd);
  [pwd wd] = min(bsxfun(@(x, y) abs(x - y), t', ti));
  wdarr = zeros(1, length(t));
  wdarr(wd(1):wd(2)) = 1;
  ret = wdarr;

  % magnitude
  magn = steps.ai.magnitude(v);

  parami = 1;

  for std_win = opts.std_win

    % std deviation
    stdev = steps.ai.std_deviation(magn, std_win);

    % std deviation threshold
    for std_th = opts.std_th
      err(parami, tracei) = sum(xor(stdev > std_th, wdarr)) / length(t);
      parami = parami + 1;
    end

  end
end

% analyse rankings
% TODO: average rank between same results
p = zeros(1, nparams);
[b, r] = sort(err);
for tracei = 1:length(traces)
  for ranki = 1:nparams
    parami = r(ranki, tracei);
    p(parami) = p(parami) + ranki;
  end
end
% average rank
p = p / length(traces);

[pmin, miniparami] = min(p);

% output stuff
me = median(err, 2);
me(miniparami)
pmin
ptable(miniparami, :)

ret = p;
