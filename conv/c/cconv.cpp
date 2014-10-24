#include <sys/stat.h>
#include <fcntl.h>
#include <ctime>
#include <iostream>
#include <iomanip>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>
#include "steps.pb.h"

using namespace std;
using namespace google::protobuf;
using namespace google::protobuf::io;

const int TIMEBUF_SIZE = 32;

struct ts_t {
  char buf[TIMEBUF_SIZE];
  int ms;
};

struct ts_t ts(long msec)
{
  struct ts_t ret;
  time_t t = msec / 1000;
  struct tm *ptm = localtime(&t);
  strftime(ret.buf, TIMEBUF_SIZE, "%Y/%m/%d %H:%M:%S", ptm);
  ret.ms = msec % 1000;
  return ret;
}

ostream& operator<<(ostream& o, struct ts_t t)
{
  return o << t.buf << '.' << setfill('0') << setw(3) << t.ms;
}

int main(int argc, char *argv[])
{
  GOOGLE_PROTOBUF_VERIFY_VERSION;

  if (argc != 2) {
    cerr << "Usage: " << argv[0] << " input" << endl;
    return -1;
  }

  stepsproto::Message msg;
  char *fname = argv[1];
  int fd = open(fname, O_RDONLY);
  FileInputStream raw(fd);
  CodedInputStream coded(&raw);

  uint32 size;
  while (coded.ReadVarint32(&size)) {
    CodedInputStream::Limit limit = coded.PushLimit(size);
    msg.ParseFromCodedStream(&coded);
    coded.PopLimit(limit);

    cout << ts(msg.timestamp() / 1000000) << ' ' << msg.sensor_id();
    switch (msg.type()) {
      case stepsproto::Message::SENSOR_EVENT:
        cout << fixed << showpoint << setprecision(8);
        for (int i = 0; i < msg.value_size(); ++i) {
          cout << ' ' << msg.value(i);
        }
        cout << endl;
        break;
      case stepsproto::Message::LOCATION:
        cout << ' ' << ts(msg.utctime()) << ' ' << fixed << showpoint;
        cout << setprecision(8);
        cout << msg.latitude() << ' ' << msg.longitude() << ' ';
        cout << setprecision(2);
        cout << msg.accuracy() << ' ' << msg.altitude() << ' ';
        cout << msg.bearing() << ' ' << msg.speed() << endl;
        break;
    }
  }
  return 0;
}


