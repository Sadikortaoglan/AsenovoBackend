-- Fix patron user password
-- This SQL script updates the patron user's password hash to match "password"
-- Run this in your PostgreSQL database

-- IMPORTANT: Replace <NEW_HASH> with a BCrypt hash generated from "password"
-- You can generate it using BCryptPasswordEncoder in your backend

-- Example (use the hash from your backend):
-- UPDATE users SET password_hash = '$2a$10$NEW_HASH_HERE' WHERE username = 'patron';

-- To generate hash in backend, create a test class:
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("password");
-- System.out.println(hash);

-- Then use that hash in the UPDATE statement below:

-- UPDATE users SET password_hash = '<GENERATED_HASH>' WHERE username = 'patron';

-- OR: Reset database completely
-- docker compose down -v
-- docker compose up -d postgres
-- # Then restart backend - seed data will run automatically

