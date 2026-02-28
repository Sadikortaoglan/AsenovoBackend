local key = KEYS[1]
local currentTime = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- Remove old entries
redis.call('ZREMRANGEBYSCORE', key, 0, currentTime - window)

-- Count current requests
local count = redis.call('ZCARD', key)

if count >= limit then
    return 0
end

-- Add current request
redis.call('ZADD', key, currentTime, currentTime)

-- Set expiration
redis.call('EXPIRE', key, window)

return 1