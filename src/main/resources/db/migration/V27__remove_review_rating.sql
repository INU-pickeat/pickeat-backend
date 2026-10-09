ALTER TABLE reviews
    DROP CONSTRAINT IF EXISTS reviews_rating,
    DROP COLUMN IF EXISTS rating;
