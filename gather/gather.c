#include <gps.h>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/epoll.h>

#define PRIVATE(gpsdata) ((struct privdata_t *)(gpsdata)->privdata)
#define CLIENT_DATE_MAX	24

struct privdata_t
{
    bool newstyle;
    /* data buffered from the last read */
    ssize_t waiting;
};

void logfix(struct gps_data_t *gpsdata) {
    static double int_time, old_int_time;

    char tbuf[CLIENT_DATE_MAX+1];

    int_time = gpsdata->fix.time;
    if ((int_time == old_int_time) || gpsdata->fix.mode < MODE_2D)
	    return;

    struct gps_fix_t* fix = &gpsdata->fix;
    printf("%s %f %.8f %.8f %f %f %f %f %f\n",
            unix_to_iso8601(fix->time, tbuf, sizeof(tbuf)),
            fix->ept, fix->latitude, fix->longitude, fix->epy, fix->epx,
            fix->altitude, fix->track, fix->speed);
    fflush(stdout);

    old_int_time = int_time;
}

void logfix_json(struct gps_data_t *gpsdata)
{
    static double int_time, old_int_time;

    char tbuf[CLIENT_DATE_MAX+1];

    int_time = gpsdata->fix.time;
    if ((int_time == old_int_time) || gpsdata->fix.mode < MODE_2D)
	    return;

    printf("{\n");
    /* seconds */
    printf("  \"time\": %s,\n",
            unix_to_iso8601(gpsdata->fix.time, tbuf, sizeof(tbuf)));
    /* seconds, 95% confidence */
    printf("  \"ept\": %f,\n", gpsdata->fix.ept);
    /* degrees */
    printf("  \"lat\": %.16f,\n", gpsdata->fix.latitude);
    /* metres, 95% confidence */
    printf("  \"epy\": %.16f,\n", gpsdata->fix.epy);
    /* degrees */
    printf("  \"lon\": %.16f,\n",  gpsdata->fix.longitude);
    /* metres, 95% confidence */
    printf("  \"epx\": %.16f,\n",  gpsdata->fix.epx);
    /* metres */
    if (!isnan(gpsdata->fix.altitude))
        printf("  \"alt\": %.16f,\n", gpsdata->fix.altitude);
    /* metres, 95% confidence */
    if (!isnan(gpsdata->fix.epv))
        printf("  \"epv\": %.16f,\n", gpsdata->fix.epv);
    /* degrees */
    if (!isnan(gpsdata->fix.track))
        printf("  \"track\": %f,\n", gpsdata->fix.track);
    /* degrees, 95% confidence */
    if (!isnan(gpsdata->fix.epd))
        printf("  \"epd\": %f,\n", gpsdata->fix.epd);
    /* m/s */
    if (!isnan(gpsdata->fix.speed))
        printf("  \"speed\": %f,\n", gpsdata->fix.speed);
    /* m/s, 95% confidence */
    if (!isnan(gpsdata->fix.eps))
        printf("  \"eps\": %f,\n", gpsdata->fix.eps);
    printf("}\n");

    old_int_time = int_time;
}

int epoll;
struct epoll_event ev[2];
struct epoll_event rev[2];

int waiting(const struct gps_data_t *gpsdata)
{
    /* all error conditions return "not waiting" -- crude but effective */
    return epoll_wait(epoll, rev, 2, -1);
}

void loop(struct gps_data_t *gpsdata)
{
    int nfds;
    char buf[256];
    int n, i;
    epoll = epoll_create1(0);
    if (epoll == -1) {
        perror("epoll_create1");
        exit(EXIT_FAILURE);
    }
    ev[0].events = EPOLLIN;
    ev[1].events = EPOLLIN;
    ev[0].data.fd = STDIN_FILENO;
    ev[1].data.fd = gpsdata->gps_fd;
    if (epoll_ctl(epoll, EPOLL_CTL_ADD, STDIN_FILENO, &ev[0])) {
        perror("epoll_ctl: stdin");
        exit(EXIT_FAILURE);
    }
    if (epoll_ctl(epoll, EPOLL_CTL_ADD, gpsdata->gps_fd, &ev[1])) {
        perror("epoll_ctl: gps");
        exit(EXIT_FAILURE);
    }

    for (;;) {
        nfds = waiting(gpsdata);
        if (nfds == -1) {
            perror("epoll_wait");
            exit(EXIT_FAILURE);
        }
        for (i = 0; i < nfds; ++i) {
            if (rev[i].data.fd == STDIN_FILENO) {
                n = read(STDIN_FILENO, buf, 256);
                if (n < 0) {
                    perror("read: stdin");
                    exit(EXIT_FAILURE);
                }
                if (n > 0 && buf[0] == 'q') {
                    return;
                }
                n = write(STDOUT_FILENO, buf, n);
                if (n < 0) {
                    perror("write: stdout");
                    exit(EXIT_FAILURE);
                }
            } else {
                int status = gps_read(gpsdata);
                if (status == -1) {
                    perror("gps_sock_read");
                    exit(EXIT_FAILURE);
                }
                logfix(gpsdata);
            }
        }
    }
}

int main(int argc, char *argv[])
{
    char *progname = argv[0];
    unsigned int flags = WATCH_ENABLE | WATCH_JSON;
    char *server = "localhost";
    char *port = DEFAULT_GPSD_PORT;
    struct gps_data_t gpsdata;
    gps_open(server, port, &gpsdata);
    if (gps_open(server, port, &gpsdata)) {
        fprintf(stderr, "%s: no gpsd running or network error: %d, %s\n",
                progname, errno, gps_errstr(errno));
        exit(EXIT_FAILURE);
    }
    gps_stream(&gpsdata, flags, NULL);
    loop(&gpsdata);
    gps_stream(&gpsdata, WATCH_DISABLE, NULL);
    gps_close(&gpsdata);
    exit(EXIT_SUCCESS);
}
