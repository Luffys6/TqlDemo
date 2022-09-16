package com.sonix.oidbluetooth.bean;

import java.io.Serializable;
import java.util.List;

public class CalligraphyResult implements Serializable {

    private DataDTO data;
    private String error;
    private String message;
    private String next;
    private String response;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public static class DataDTO {
        private Content content;
        private String img_url;
        private Integer index_pos;
        private List<PosListDTO> pos_list;
        private Integer score;
        private String word;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getImg_url() {
            return img_url;
        }

        public void setImg_url(String img_url) {
            this.img_url = img_url;
        }

        public Integer getIndex_pos() {
            return index_pos;
        }

        public void setIndex_pos(Integer index_pos) {
            this.index_pos = index_pos;
        }

        public List<PosListDTO> getPos_list() {
            return pos_list;
        }

        public void setPos_list(List<PosListDTO> pos_list) {
            this.pos_list = pos_list;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public static class Content {
            private String content;
            private List<ListDTO> list;

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public List<ListDTO> getList() {
                return list;
            }

            public void setList(List<ListDTO> list) {
                this.list = list;
            }

            public static class ListDTO {
                private List<ContentDTO> content;
                private String name;

                public List<ContentDTO> getContent() {
                    return content;
                }

                public void setContent(List<ContentDTO> content) {
                    this.content = content;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public static class ContentDTO {
                    private String content;
                    private Integer sequence;

                    public String getContent() {
                        return content;
                    }

                    public void setContent(String content) {
                        this.content = content;
                    }

                    public Integer getSequence() {
                        return sequence;
                    }

                    public void setSequence(Integer sequence) {
                        this.sequence = sequence;
                    }
                }
            }
        }

        public static class PosListDTO {
            private Integer index;
            private List<MovePointDTO> movePoint;

            public Integer getIndex() {
                return index;
            }

            public void setIndex(Integer index) {
                this.index = index;
            }

            public List<MovePointDTO> getMovePoint() {
                return movePoint;
            }

            public void setMovePoint(List<MovePointDTO> movePoint) {
                this.movePoint = movePoint;
            }

            public static class MovePointDTO {
                private Integer x;
                private Integer y;

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
            }
        }
    }
}
