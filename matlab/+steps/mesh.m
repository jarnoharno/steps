function z = mesh(f, x, y)
[xy yx] = meshgrid(x, y);
z = bsxfun(f, xy, yx);
