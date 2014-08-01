function ret = load_brajdic_all()

% Possible modalities:
%
% Backpack
% Hand_held
% Handbag
% Handheld_using
% Shirt_pocket
% Trousers_back_pocket
% Trousers_front_pocket

% data directory
base = 'data/brajdic/';

% load step count ground truths
sc = readtable([base 'groundtruth_SC.txt'], 'Delimiter', ' ', ...
  'ReadVariableNames', false, 'ReadRowName', true, 'Format', '%s %d');
sc.Properties.VariableNames = {'steps'};

% load walk detection ground truths
wd = readtable([base 'groundtruth_WD.txt'], 'Delimiter', ' ', ...
  'ReadVariableNames', false, 'ReadRowName', true, ...
  'HeaderLines', 2, 'Format', '%s%*[ ](%d %d)');
wd.Properties.VariableNames = {'first' 'last'};

% data files
list = ls(base);
files = strsplit(list,{'\t','\n'});
files(end) = []; % remove last

% remove groundtruth files
files(strncmp(files, 'groundtruth', 11)) = [];

short = '^p\d+\.\d';
discard = '(_[^-]*-[^-]*-[^_]*_)';
extension = '(\.[^.]*$)';

ret = struct;
files = sort(files);
n = size(files, 2);

for i = 1:n
  file = files{i};
  fprintf('%s %d/%d\n', file, i, n);

  longname = regexprep(file, [discard '|' extension], '');
  shortname = regexp(longname, short, 'match');
  shortname = shortname{1};

  % construct fieldname
  fieldname = strrep(longname, '.', '_');
  fieldname = regexprep(fieldname, '^p(\d)_', 'p0$1_');
  fieldname = strrep(fieldname, 'Backpack', '_bp');
  fieldname = strrep(fieldname, 'Hand_held', '_hh');
  fieldname = strrep(fieldname, 'Handbag', '_hb');
  fieldname = strrep(fieldname, 'Handheld_using', '_hu');
  fieldname = strrep(fieldname, 'Shirt_pocket', '_sp');
  fieldname = strrep(fieldname, 'Trousers_back_pocket', '_tb');
  fieldname = strrep(fieldname, 'Trousers_front_pocket', '_tf');

  ret.(fieldname) = struct('data', steps.load_brajdic([base file]),...
    'file', file);

  if any(strcmp(sc.Properties.RowNames, shortname))
    ret.(fieldname).sc = sc.steps(shortname);
  end

  if any(strcmp(wd.Properties.RowNames, longname))
    ret.(fieldname).wd = wd{longname, {'first', 'last'}};
  end
end
ret = orderfields(ret);
