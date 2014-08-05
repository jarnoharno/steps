function ret = find(v, vq)
ret = arrayfun(@(x) find(x == v), vq);
