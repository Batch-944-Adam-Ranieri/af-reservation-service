# af-reservation-service
Microservice responsible for reserving space among Revature's digital and physical 
facilities. Part of the AssignForce suite

## Environment Variables
This repository requires one environment variable `AUTH_SERVER` for deployment
It should be in the form of `${name-of-authorization-service-here}/verify`. The authorization service
should also be deployed on the same K8 cluster so that discovery is possible.

## Routes, Requests and Responses
All requests should have an Authorization header with a valid JWT, and will be rejected 
otherwise. Any request that does not have a JWT will be returned a `401 UNAUTHORIZED`. 
Most requests return a reservation object or list of reservation objects, each of which 
will be in this form.

```json
{
  "reservationId": "Numeric ID",
  "reserver": "Email string",
  "startTime": "Unix Epoch timestamp as number",
  "endTime": "Unix Epoch timestamp as number",
  "status": "reserved | cancelled",
  "roomId": "Numeric Room Id from path",
  "title": "Title of the Reservation"
}
```

### Create new Reservation
`POST /reservations`

Expected Body:
```json
{
  "startTime": "Unix Epoch timestamp as number",
  "endTime": "Unix Epoch timestamp as number",
  "roomId": "ID of a valid room in the database",
  "title": "Optional title, will be made reserver email if none is given"
}
```
Possible returns:
- `201` with full reservation return body
- `404 NOT FOUND` if invalid or no room ID is provided
- `406 NOT ACCEPTABLE` if startTime is after endTime
- `409 CONFLICT` if given times conflict with times in the database

### Get All Reservations
`GET /reservations?start=number&end=number&reserver=string&status=string&roomId=number`

Gets all reservations. Users can filter based on the optional parameters. Optional parameters
include:
- `start (Unix epoch time)` Set a floor for reservation times
- `end (Unix epoch time)` Set a ceiling for reservation times
- `reserver (Email string)` Search for only reservations made by a specific reserver
- `status (Status string)` Search for only reservations with a given status. Valid statuses
include `reserved` and `cancelled`.
- `roomId (Integer)` Search for only reservations within a given room.

It is recommended that some time range is always given when making calls to this endpoint, as
not doing so will slow down response times considerably.

Possible returns:
- `200` with list of reservations bodies
- `406 NOT ACCEPTABLE` if start is after end

### Get Reservation By Reservation ID
`GET /reservations/{reservationId}`

Gets reservation with `reservationId`.

Possible returns:
- `200` with full reservation return body
- `404 NOT FOUND` if invalid reservation ID is provided in the path

### Update Reservation By Reservation ID
`PATCH /reservations/{reservationId}?action=cancel`

This method either, changes the times for a reservation in a room, or cancels a reservation
in the database if the action query param is set to "cancel". If action is set to cancel,
the body is ignored, if the action is set to anything else, action is ignored. All the 
body elements for updating are optional and if any are omitted, the old value will persist.

Expected Body:
```json
{
  "startTime": "Unix Epoch timestamp as number (Optional)",
  "endTime": "Unix Epoch timestamp as number (Optional)",
  "title": "Title string to be updated to (Optional)"
}
```

Possible returns:
- `200` with full reservation return body
- `404 NOT FOUND` if invalid reservation ID is provided
- `406 NOT ACCEPTABLE` if startTime is after endTime
- `403 FORBIDDEN` if user is attempting to change a reservation that isn't theirs
    * `admin` users can both cancel and update other users reservations. Updating another user's reservation will 
    transfer ownership of the reservation to the admin user. This may need change in the future.
- `409 CONFLICT` if the new times have a conflict with another reservation