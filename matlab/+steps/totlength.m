function ret = totlength(d, trange)
ret = [];
gn = size(d.gps.t,1);
sn = length(d.map.t);
% segment length 10 s
seglent = int64(1e10);
% segment gps indices
gi1 = 1;
gi2 = 1;
% segment step (map) indices
si = 1;
si1 = 1;
si2 = 1;
% find segment start
for gi = 1:gn
  % find gps segment start
  if d.gps.t(gi,1) >= trange(1)
    gi1 = gi;
    % find step segment start
    while si <= sn
      if d.map.t(si) >= d.gps.t(gi,1)
        si1 = si;
        break;
      end
      si = si+1;
    end
    break;
  end
end

% loop through segments
for gi = gi1:gn
  if d.gps.t(gi,1) >= d.gps.t(gi1,1) + seglent
    gi2 = gi;
    while si <= sn
      if d.map.t(si) >= d.gps.t(gi2,1)
        si2 = si;
        break;
      end
      si = si+1;
    end
    break;
  end
end

% calculate segment data
seglen = 0;
for gi = gi1:gi2-1
  seglen = seglen + haversine(d.gps.v(gi,1:2),d.gps.v(gi+1,1:2));
end
halfstep1 = double(d.map.t(si1)-d.gps.t(gi1,1)) / double(d.map.t(si1)-d.map.t(si1-1));
halfstep2 = double(d.map.t(si2)-d.gps.t(gi2,1)) / double(d.map.t(si2)-d.map.t(si2-1));
steps = si2-si1+halfstep1+halfstep2;
ret = [ret; seglen steps];
