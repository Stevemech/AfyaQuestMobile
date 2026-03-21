#!/usr/bin/env bash
# Delete every DynamoDB item whose SK starts with ASSIGNMENT# (all user tasks).
# Requires: aws CLI, jq
#
# Usage:
#   export AWS_REGION=af-south-1   # must match your AfyaQuestData table region
#   export DYNAMODB_TABLE=AfyaQuestData
#   ./scripts/delete-all-dynamodb-assignments.sh
#
# Dry run (list only, no deletes):
#   DRY_RUN=1 ./scripts/delete-all-dynamodb-assignments.sh

set -euo pipefail

REGION="${AWS_REGION:-af-south-1}"
TABLE="${DYNAMODB_TABLE:-AfyaQuestData}"
DRY_RUN="${DRY_RUN:-0}"

if ! command -v jq &>/dev/null; then
  echo "jq is required. Install jq or use the AWS Console / a one-off script."
  exit 1
fi

echo "Table: $TABLE  Region: $REGION  DRY_RUN=$DRY_RUN"
TOTAL=0
LEK_FILE="$(mktemp)"
trap 'rm -f "$LEK_FILE"' EXIT

while true; do
  if [[ ! -s "$LEK_FILE" ]]; then
    PAGE=$(aws dynamodb scan \
      --region "$REGION" \
      --table-name "$TABLE" \
      --filter-expression "begins_with(SK, :p)" \
      --expression-attribute-values '{":p":{"S":"ASSIGNMENT#"}}' \
      --projection-expression "PK,SK" \
      --output json)
  else
    PAGE=$(aws dynamodb scan \
      --region "$REGION" \
      --table-name "$TABLE" \
      --filter-expression "begins_with(SK, :p)" \
      --expression-attribute-values '{":p":{"S":"ASSIGNMENT#"}}' \
      --projection-expression "PK,SK" \
      --exclusive-start-key "$(cat "$LEK_FILE")" \
      --output json)
  fi

  COUNT=$(echo "$PAGE" | jq '.Items | length')
  echo "$PAGE" | jq -c '.Items[]?' | while read -r row; do
    PK=$(echo "$row" | jq -c '.PK')
    SK=$(echo "$row" | jq -c '.SK')
    echo "  delete PK=$PK SK=$SK"
    if [[ "$DRY_RUN" != "1" ]]; then
      aws dynamodb delete-item \
        --region "$REGION" \
        --table-name "$TABLE" \
        --key "{\"PK\":$PK,\"SK\":$SK}"
    fi
  done

  TOTAL=$((TOTAL + COUNT))

  LEK=$(echo "$PAGE" | jq -c 'if .LastEvaluatedKey then .LastEvaluatedKey else empty end')
  if [[ -z "$LEK" || "$LEK" == "null" ]]; then
    break
  fi
  echo "$LEK" >"$LEK_FILE"
done

echo "Done. Scanned/deleted batch total items in last pages: $TOTAL (see per-page counts above)."
echo "Note: Run again if the table is large and you need to verify; scan is eventually consistent."
