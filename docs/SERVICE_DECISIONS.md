# Pick Eat Service Decisions

Last updated: 2026-09-17

## Product source of truth

The current Pick Eat product plan is the source of truth. Older survey-product documents and former exhibition-only restaurant decisions are retained only as historical references.

## Restaurant data strategy

- Recommendation candidates come from Google Places nearby search.
- The service stores the minimum restaurant snapshot needed for stable internal IDs, picks, reviews, and external-data refreshes.
- `GOOGLE` and `CURATED` are recorded separately through `data_provider`.
- Curated data is reserved for the initial explore experience: five restaurants in each of Yeonnam, Hannam, Seochon, Sinsa, and Nonhyeon (25 total).
- Google ratings and future Pick Eat ratings remain separate because they have different meanings and update policies.

### Google primary type → FoodCategory mapping (M1-2, validated 2026-09-20)

Validated against ~99 live `searchNearby` results (radius 800m, `POPULARITY`) across Yeonnam, Hannam, Seochon, Sinsa, and Nonhyeon. Kept in sync with `FoodCategory.fromGooglePrimaryType()` in code.

| FoodCategory | Google primaryType |
|---|---|
| KOREAN | `korean_restaurant`, `korean_barbecue_restaurant`, `chicken_restaurant`, `chicken_wings_restaurant`, `seafood_restaurant`, `noodle_shop` |
| JAPANESE | `japanese_restaurant`, `ramen_restaurant` |
| CHINESE | `chinese_restaurant` |
| WESTERN | `western_restaurant`, `italian_restaurant`, `european_restaurant`, `sandwich_shop`, `irish_pub`, `brunch_restaurant` |
| CAFE_DESSERT | `cafe`, `dessert_shop` |

Unmapped-type policy (resolves roadmap decision item 1): **excluded**, not bucketed into a placeholder "other" category. Excluded types carry no reliable single-cuisine signal in the sample: the generic `restaurant` type (~15% of results, e.g. fried-chicken chains and home-style Korean diners with no specific Google subtype), dietary/format labels (`halal_restaurant`, `vegan_restaurant`, `buffet_restaurant`, `fine_dining_restaurant`), cuisines outside the 5 categories (`indian_restaurant`, `turkish_restaurant`), `asian_fusion_restaurant`, and `meal_takeaway`.

Data quality findings from the sample:
- Search success rate: 5/5 regions returned results.
- Rating present: ~100% of results.
- Opening hours present: ~93% of results.
- Mismatch: `hotel` appeared repeatedly despite `includedTypes: ["restaurant"]` (hotel entries with a dining venue indexed under the hotel place) — excluded, not a food category.

## Recommendation baseline

- The search radius is fixed at 5 km and uses straight-line distance.
- The radius does not expand automatically when fewer than five candidates exist.
- Food categories are Korean, Japanese, Chinese, Western, and Cafe/Dessert.
- A request may contain multiple food categories and exactly one companion type.
- Companion suitability is a score bonus, not an exclusion filter.
- The target result size is five restaurants; the behavior for fewer candidates belongs to the recommendation implementation task.

## API and operations

- Public authentication endpoints use `/api/v1/auth/signup` and `/api/v1/auth/login`.
- All other business endpoints require a bearer token unless explicitly documented otherwise.
- Exception: `GET /api/v1/restaurants/**` is public. Restaurant detail is non-sensitive, and shared restaurant links must open for logged-out users; write/mutating restaurant-related actions still require a bearer token.
- OpenAPI and Swagger UI are enabled only with the `local` Spring profile.
- Schema changes continue after the existing Member history: Restaurant uses Flyway V5 and V6.

## Implementation status

- Implemented: Member signup/login, JWT filter, Restaurant persistence model, PostGIS location column and index, companion suitability flags, CI database service, restaurant detail API, Google Places client, Google primary type category mapping.
- Next: Google Restaurant upsert/refresh policy, 5 km repository query, navigation-link API, recommendation scoring and session API.
