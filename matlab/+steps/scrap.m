function ret = scrap()

a = 0;

function ret = nested()
a = a + 1;
ret = a;
end

ret = @nested;
end
