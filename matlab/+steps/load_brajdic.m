% http://www.cl.cam.ac.uk/~ab818/parseTraces.py
function ret = load_brajdic(filename)

if (isempty(regexp(filename, 'out$')))
  % .dat
  ret = steps.load_brajdic_txt(filename);
else
  % .out
  ret = steps.load_brajdic_bin(filename);
end
