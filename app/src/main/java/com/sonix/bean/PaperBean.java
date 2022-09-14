package com.sonix.bean;

import java.util.List;

public class PaperBean {


    private String response;
    private DataDTO data;
    private String error;
    private String next;
    private String message;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class DataDTO {
        private Integer img_id;
        private Integer w;
        private Integer h;
        private String img_url;
        private List<PosListDTO> pos_list;

        public Integer getImg_id() {
            return img_id;
        }

        public void setImg_id(Integer img_id) {
            this.img_id = img_id;
        }

        public Integer getW() {
            return w;
        }

        public void setW(Integer w) {
            this.w = w;
        }

        public Integer getH() {
            return h;
        }

        public void setH(Integer h) {
            this.h = h;
        }

        public String getImg_url() {
            return img_url;
        }

        public void setImg_url(String img_url) {
            this.img_url = img_url;
        }

        public List<PosListDTO> getPos_list() {
            return pos_list;
        }

        public void setPos_list(List<PosListDTO> pos_list) {
            this.pos_list = pos_list;
        }

        public static class PosListDTO {
            private Integer x;
            private Integer y;
            private Integer ax;
            private Integer ay;

            public PosListDTO(Integer x, Integer y, Integer ax, Integer ay) {
                this.x = x;
                this.y = y;
                this.ax = ax;
                this.ay = ay;
            }

            public Integer getX() {
                return x;
            }

            public void setX(Integer x) {
                this.x = x;
            }

            public Integer getY() {
                return y;
            }

            public void setY(Integer y) {
                this.y = y;
            }

            public Integer getAx() {
                return ax;
            }

            public void setAx(Integer ax) {
                this.ax = ax;
            }

            public Integer getAy() {
                return ay;
            }

            public void setAy(Integer ay) {
                this.ay = ay;
            }
        }
    }
}
