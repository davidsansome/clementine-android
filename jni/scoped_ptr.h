/* This file is part of Clementine.
   Copyright 2012, David Sansome <me@davidsansome.com>
   
   Clementine is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   Clementine is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with Clementine.  If not, see <http://www.gnu.org/licenses/>.
*/

#ifndef SCOPED_PTR_H
#define SCOPED_PTR_H

template<class T> class scoped_ptr {
 public:
  explicit scoped_ptr(T* p = 0)
    : data_(p) {
  }

  ~scoped_ptr() {
    delete data_;
  }

  void reset(T* p = 0) {
    delete data_;
    data_ = p;
  }

  T& operator *() const {
    return *data_;
  }

  T* operator ->() const {
    return data_;
  }

  T* get() const {
    return data_;
  }

  operator bool() const {
    return data_;
  }

 private:
  scoped_ptr(const scoped_ptr<T>& other) {}
  void operator =(const scoped_ptr<T>& other) {}

  T* data_;
};

#endif // SCOPED_PTR_H
