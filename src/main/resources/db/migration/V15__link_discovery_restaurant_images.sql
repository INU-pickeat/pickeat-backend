UPDATE restaurants restaurant
SET representative_image_url = '/images/discovery/'
    || LOWER(spot.region_code)
    || '_'
    || LPAD(spot_restaurant.display_order::TEXT, 2, '0')
    || '_main.jpg'
FROM discovery_spot_restaurants spot_restaurant
JOIN discovery_spots spot ON spot.id = spot_restaurant.discovery_spot_id
WHERE restaurant.id = spot_restaurant.restaurant_id;
