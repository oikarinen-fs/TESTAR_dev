from flask import Flask
import os

app = Flask(__name__)

# Register reporter
# TODO: Implement more efficient way to enable this feature
if os.environ.get("ADAPTER") in ["RANDOM", "MYSQL"]:
    from reporting.views import reporting

    app.register_blueprint(reporting)

if __name__ == "__main__":
    print(" * Running without UWSGI")


    # TODO: Remove static variables
    app.run(port=8000, host="0.0.0.0")
