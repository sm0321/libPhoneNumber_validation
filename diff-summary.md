## Snapshot diff

전체 `diff.json`은 이 워크플로 실행의 **Artifacts → snapshot-diff** 에서 받을 수 있습니다.

- Run: https://github.com/sm0321/libPhoneNumber_validation/actions/runs/25904322530

### 요약
```json
{
  "total": 3000,
  "changes_count": 104
}
```

### 변경 샘플 (최대 5건)
```json
[
  {
    "type": "CHANGED",
    "index": 67,
    "input": "17367770319",
    "region": "DE",
    "before": {
      "input": "17367770319",
      "region": "DE",
      "caseType": "VALID",
      "valid": false,
      "type": "UNKNOWN",
      "e164": "+4917367770319",
      "countryCode": 49,
      "nationalNumber": 17367770319
    },
    "after": {
      "valid": true,
      "type": "MOBILE"
    }
  },
  {
    "type": "CHANGED",
    "index": 89,
    "input": "17024025131",
    "region": "DE",
    "before": {
      "input": "17024025131",
      "region": "DE",
      "caseType": "VALID",
      "valid": false,
      "type": "UNKNOWN",
      "e164": "+4917024025131",
      "countryCode": 49,
      "nationalNumber": 17024025131
    },
    "after": {
      "valid": true,
      "type": "MOBILE"
    }
  },
  {
    "type": "CHANGED",
    "index": 116,
    "input": "17371927469",
    "region": "DE",
    "before": {
      "input": "17371927469",
      "region": "DE",
      "caseType": "VALID",
      "valid": false,
      "type": "UNKNOWN",
      "e164": "+4917371927469",
      "countryCode": 49,
      "nationalNumber": 17371927469
    },
    "after": {
      "valid": true,
      "type": "MOBILE"
    }
  },
  {
    "type": "CHANGED",
    "index": 147,
    "input": "17132460114",
    "region": "DE",
    "before": {
      "input": "17132460114",
      "region": "DE",
      "caseType": "VALID",
      "valid": false,
      "type": "UNKNOWN",
      "e164": "+4917132460114",
      "countryCode": 49,
      "nationalNumber": 17132460114
    },
    "after": {
      "valid": true,
      "type": "MOBILE"
    }
  },
  {
    "type": "CHANGED",
    "index": 162,
    "input": "17375611924",
    "region": "DE",
    "before": {
      "input": "17375611924",
      "region": "DE",
      "caseType": "VALID",
      "valid": false,
      "type": "UNKNOWN",
      "e164": "+4917375611924",
      "countryCode": 49,
      "nationalNumber": 17375611924
    },
    "after": {
      "valid": true,
      "type": "MOBILE"
    }
  }
]
```
