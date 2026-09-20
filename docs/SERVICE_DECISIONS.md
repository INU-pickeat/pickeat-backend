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

- Implemented: Member signup/login, JWT filter, Restaurant persistence model, PostGIS location column and index, companion suitability flags, CI database service.
- Next: Google Places client and category mapping, 5 km repository query, restaurant detail/navigation API, recommendation scoring and session API.
