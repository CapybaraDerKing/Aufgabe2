import requests
import time
import json

# URL zum Abrufen der Daten
DATA_URL = "http://assessment:8080/v1/dataset"
RESULT_URL = "http://assessment:8080/v1/result"

def fetch_data():
    response = requests.get(DATA_URL)
    return response.json()

def calculate_runtime(events):
    customer_runtime = {}
    workload_start_times = {}

    for event in events:
        customer_id = event['customerId']
        workload_id = event['workloadId']
        timestamp = event['timestamp']
        event_type = event['eventType']

        if event_type == 'start':
            workload_start_times[workload_id] = timestamp
        elif event_type == 'stop' and workload_id in workload_start_times:
            start_time = workload_start_times.pop(workload_id)
            runtime = timestamp - start_time
            if customer_id in customer_runtime:
                customer_runtime[customer_id] += runtime
            else:
                customer_runtime[customer_id] = runtime

    return [{"customerId": customer_id, "consumption": runtime} for customer_id, runtime in customer_runtime.items()]

def send_result(result):
    # Debugging: Überprüfen, was genau gesendet wird
    print(f"Sending JSON: {json.dumps({'result': result}, indent=2)}")
    
    response = requests.post(RESULT_URL, json={"result": result})
    print(f"Result sent with status code: {response.status_code}")
    
    return response.status_code

if __name__ == "__main__":
    while True:
        data = fetch_data()
        events = data.get("events", [])
        result = calculate_runtime(events)
        status_code = send_result(result)
        print(f"Result sent with status code: {status_code}")
        time.sleep(10)
