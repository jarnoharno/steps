function ret = flatten(arr)
ret = reshape(arr, [1 prod(size(arr))]);
