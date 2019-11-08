FROM busybox
ENV DOCKERFILE_FOO /bar
WORKDIR ${foo}   # WORKDIR /bar
ADD . $foo       # ADD . /bar
COPY \$foo /quux # COPY $foo /quux
ENV ADMIN_USER_DOCKERFILE="mark"