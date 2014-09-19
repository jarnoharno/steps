module Steps

using DataFrames
using Iterators
using Dates

export
    readtrace,
    haversine,
    totlength,
    lengths,
    binsearch,
    findrange,
    cutrange,
    cutall,
    findpeaks

type Peak
    index::Int
    included::Bool
    prev::Peak
    next::Peak
    Peak(i) = new(i,true)
end

Main.show(stream::IO,p::Peak)=print(stream,"Peak($(p.index),$(p.included))")

function findpeaks(x, minpeakdist=0, distf=(i,j)->j-i,span=(1,size(x,1)))
    peaks = Peak[]
    root = Steps.Peak(0)
    root.prev = root
    root.next = root
    prev = root

    # find peaks
    for i = span[1]+1:span[2]-1
        if x[i-1] < x[i] > x[i+1]
            p = Peak(i)
            p.prev = prev
            p.next = root
            prev.next = p
            prev = p
            push!(peaks,p)
        end
    end

    # sort peaks
    sort!(peaks,by=p->x[p.index],rev=true)

    # prune peaks that are too close
    for i = 1:length(peaks)
        p = peaks[i]
        if !p.included
            continue
        end
        # prune left side
        while p.prev != root && distf(p.prev.index, p.index) < minpeakdist
            p.prev.included = false
            p.prev = p.prev.prev
            p.prev.next = p
        end
        # check if first peak in range
        if p.prev == root
            root.next = p
        end
        # prune right side
        while p.next != root && distf(p.index, p.next.index) < minpeakdist
            p.next.included = false
            p.next = p.next.next
            p.next.prev = p
        end
    end

    # gather peak indices
    ret = Int[]
    p = root.next
    while p != root
        push!(ret, p.index)
        p = p.next
    end
    ret
end

# calculate the indices of array `x` that are closest to value `v`.
# return [infimum, supremum], if the exact value was found, both indices
# are the same. otherwise, infimum > supremum.
function binsearch(x, v)
    low = 1
    high = size(x, 1)
    while low <= high
        mid = int((low+high)/2)
        if x[mid] < v
            low = mid+1
        elseif x[mid] > v
            high = mid-1
        else
            return [mid,mid]
        end
    end
    return [low,high]
end

function findrange(x,range)
    low = binsearch(x,range[1])[1]
    high = binsearch(x,range[end])[end]
    [low,high]
end

function cutrange(df,range,rangesym=:t)
    drange = findrange(df[rangesym],range)
    df[colon(drange...),:]
end

function cutall(d,range,rangesym=:t)
    ret = Dict{Symbol,DataFrame}()
    for i = d
        ret[i[1]] = cutrange(i[2],range,rangesym)
    end
    ret
end

function mergeutrace(d,ut)
    n = size(ut,1)
    t = Array(Int64,n)
    println(n)
    for i = 1:n
        li = Steps.binsearch(d[:gps][:u],ut[i,:u])
        if li[1] > size(d[:gps],1)
            v = d[:gps][li[end],:t] + (ut[i,:u] - d[:gps][li[end],:u])
        elseif li[1] - li[end] > 0
            v = d[:gps][li[end],:t] + diff(d[:gps][li,:t])[1] *
                (ut[i,:u] - d[:gps][li[end],:u]) / diff(d[:gps][li,:u])[1]
        else
            v = d[:gps][li[1],:t]
        end
        t[i] = int64(v)
    end
    ut[:t] = t
    d[:ubx] = ut
    d
end

haversine(df) = haversine(df[1,:lat],df[1,:lon],df[2,:lat],df[2,:lon])
haversine(a, b) = haversine(a[1], a[2], b[1], b[2])
haversine(lat1,lon1,lat2,lon2) = 2 * 6372.8e3 *
    asin(sqrt(sind((lat2-lat1)/2)^2 + cosd(lat1) * cosd(lat2) *
    sind((lon2 - lon1)/2)^2))

totlength(df::AbstractDataFrame, lenf=haversine) =
    totlength(df[:lat],df[:lon],lenf)
function totlength(lat, lon, lenf=haversine)
    ret = 0.0
    for i = 1:length(lat)-1
        ret += lenf(lat[i],lon[i],lat[i+1],lon[i+1])
    end
    ret
end

lengths(df::AbstractDataFrame, lenf=haversine) =
    lengths(df[:lat],df[:lon],lenf)
function lengths(lat, lon, lenf=haversine)
    n = length(lat)-1
    ret = zeros(Float64,n)
    for i = 1:n
        ret[i] = lenf(lat[i],lon[i],lat[i+1],lon[i+1])
    end
    ret
end

locm = r"gps|net|unk"
locf = [
    (:tag,String),
    (:t,Int64),
    (:u,Int64),
    (:lat,Float64),
    (:lon,Float64),
    (:acc,Float64),
    (:alt,Float64),
    (:bea,Float64),
    (:spe,Float64)
]

function readfunc(t::DataType)
    if t == Float64
        float64
    else
        int64
    end
end

function readutrace(file::String)
    dt = DataFrame(u=Int64[],lat=Float64[],lon=Float64[],
        acc=Float64[],alt=Float64[],bea=Float64[],spe=Float64[])
    open(file) do f
       for line = eachline(f)
           words = split(line)
           u = int64(datetime2unix(DateTime(words[1],"y-m-dTH:M:S.sZ")))*1000
           # accuracy is the geometric mean of epx and epy divided by two
           # (95% confidence -> 68%)
           acc = sqrt(prod(float64(words[[5,6]])))/2
           push!(dt[:u],u)
           push!(dt[:lat],float64(words[3]))
           push!(dt[:lon],float64(words[4]))
           push!(dt[:acc],acc)
           push!(dt[:alt],float64(words[7]))
           push!(dt[:bea],float64(words[8]))
           push!(dt[:spe],float64(words[9]))
       end
    end
    dt
end

function readtrace(file::String)
    d = Dict{Symbol,DataFrame}()
    open(file) do f
        for line = eachline(f)
            words = split(line)
            tag = words[1]
            df = get!(d,symbol(tag)) do
                if ismatch(locm,tag)
                    df = DataFrame()
                    for i = 2:length(locf)
                        df[locf[i][1]] = locf[i][2][]
                    end
                    return df
                elseif length(words) < 4
                    return DataFrame(t=Int64[],v=Float64[])
                else
                    return DataFrame(t=Int64[],v=Array{Float64,1}[])
                end
            end
            if ismatch(locm,tag)
                for i = 2:length(locf)
                    push!(df[locf[i][1]], readfunc(locf[i][2])(words[i]))
                end
            elseif length(words) < 4
                push!(df[:t], int64(words[2]))
                push!(df[:v], float64(words[3]))
            else
                push!(df[:t], int64(words[2]))
                v = map(words[3:end]) do w
                    float64(w)
                end
                push!(df[:v],v)
            end
        end
    end
    return d
end

end # module
