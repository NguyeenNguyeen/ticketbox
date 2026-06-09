#!/bin/bash
# 1. Register a test user
EMAIL="daonguyennguyen2k5@gmail.com"
USERNAME="e2e_user_$(date +%s)"
curl -s -X POST "http://localhost:8080/api/auth/register" -H "Content-Type: application/json" -d "{\"username\": \"$USERNAME\", \"password\": \"pass\", \"email\": \"$EMAIL\"}" > /dev/null

# 2. Login
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" -H "Content-Type: application/json" -d "{\"username\": \"$USERNAME\", \"password\": \"pass\"}" | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')

# 3. Create a ticket category using direct DB insertion because I don't know the exact endpoint
psql -U postgres -d ticketbox -c "INSERT INTO concerts (name, description, location, start_time, end_time) VALUES ('E2E Concert', 'Test', 'Venue', NOW() + INTERVAL '1 day', NOW() + INTERVAL '2 days') RETURNING id;" > concert_id.txt 2>/dev/null
CID=$(grep -o '[0-9]*' concert_id.txt | head -1)
psql -U postgres -d ticketbox -c "INSERT INTO ticket_categories (concert_id, name, price, total_quantity, available_quantity) VALUES ($CID, 'VIP', 100, 10, 10) RETURNING id;" > cat_id.txt 2>/dev/null
CATID=$(grep -o '[0-9]*' cat_id.txt | head -1)

# 4. Purchase Ticket
curl -s -X POST "http://localhost:8080/api/tickets/purchase" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"categoryId\": $CATID, \"quantity\": 1}" > purchase_res.json
cat purchase_res.json
